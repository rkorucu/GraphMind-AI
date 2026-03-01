import { useEffect, useState, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { analyzeStream } from "../api/analyzeService";

// Agent metadata for display
const AGENT_META = {
  planner: {
    label: "Planner",
    icon: "📋",
    desc: "Decomposing objective into an execution plan",
  },
  research: {
    label: "Research",
    icon: "🔬",
    desc: "Gathering data using market & financial tools",
  },
  risk: {
    label: "Risk Assessment",
    icon: "⚠️",
    desc: "Evaluating risks, severity & mitigations",
  },
  critic: {
    label: "Critic Review",
    icon: "🧐",
    desc: "Reviewing quality — approve, revise, or reject",
  },
  synthesizer: {
    label: "Synthesizer",
    icon: "✨",
    desc: "Combining outputs into a final response",
  },
};

const PIPELINE_ORDER = ["planner", "research", "risk", "critic"];

export default function Timeline() {
  const location = useLocation();
  const navigate = useNavigate();
  const question = location.state?.question;

  // Each step: { node, status, durationMs, loop }
  const [steps, setSteps] = useState([]);
  const [activeAgent, setActiveAgent] = useState(null);
  const [currentLoop, setCurrentLoop] = useState(0);
  const [error, setError] = useState(null);
  const [phase, setPhase] = useState("running"); // running | done | error
  const pipelineIndexRef = useRef(0);
  const loopRef = useRef(0);
  const stepsRef = useRef([]);

  useEffect(() => {
    if (!question) {
      navigate("/");
      return;
    }

    // Show first agent as active immediately for responsiveness
    const firstAgent = PIPELINE_ORDER[0];
    const initialStep = {
      node: firstAgent,
      status: "active",
      durationMs: null,
      loop: 0,
    };
    setSteps([initialStep]);
    stepsRef.current = [initialStep];
    setActiveAgent(firstAgent);
    pipelineIndexRef.current = 0;

    analyzeStream(question, (event) => {
      const { node } = event;
      const meta = AGENT_META[node];
      if (!meta) return; // skip unknown nodes

      // Mark the current step as done
      const updatedSteps = stepsRef.current.map((s) =>
        s.status === "active"
          ? { ...s, status: "done", durationMs: event.durationMs }
          : s,
      );

      // Detect revision loop: if we see "planner" again after seeing "critic"
      const hadCritic = stepsRef.current.some(
        (s) => s.node === "critic" && s.loop === loopRef.current,
      );
      if (node === "planner" && hadCritic) {
        loopRef.current += 1;
        setCurrentLoop(loopRef.current);
      }

      // Is this the same node that's already active? (already added as placeholder)
      const isAlreadyActive = updatedSteps.some(
        (s) =>
          s.node === node &&
          s.status === "active" &&
          s.loop === loopRef.current,
      );

      if (!isAlreadyActive) {
        // Add this agent as done
        updatedSteps.push({
          node,
          status: "done",
          durationMs: event.durationMs,
          loop: loopRef.current,
        });
      } else {
        // Mark the placeholder as done
        for (let i = updatedSteps.length - 1; i >= 0; i--) {
          if (
            updatedSteps[i].node === node &&
            updatedSteps[i].status === "active"
          ) {
            updatedSteps[i] = {
              ...updatedSteps[i],
              status: "done",
              durationMs: event.durationMs,
            };
            break;
          }
        }
      }

      // Predict and show the next agent as active
      const nextAgent = predictNextAgent(node, loopRef.current);
      if (nextAgent) {
        updatedSteps.push({
          node: nextAgent,
          status: "active",
          durationMs: null,
          loop: loopRef.current,
        });
        setActiveAgent(nextAgent);
      }

      stepsRef.current = updatedSteps;
      setSteps([...updatedSteps]);
    })
      .then((result) => {
        // Mark all remaining active steps as done
        const finalSteps = stepsRef.current.map((s) =>
          s.status === "active" ? { ...s, status: "done" } : s,
        );
        stepsRef.current = finalSteps;
        setSteps(finalSteps);
        setPhase("done");
        setActiveAgent(null);

        setTimeout(() => {
          navigate("/result", { state: { question, result } });
        }, 800);
      })
      .catch((err) => {
        setError(err.message);
        setPhase("error");
        const errorSteps = stepsRef.current.map((s) =>
          s.status === "active" ? { ...s, status: "error" } : s,
        );
        stepsRef.current = errorSteps;
        setSteps(errorSteps);
      });
  }, [question, navigate]);

  // Predict which agent runs next based on the pipeline order
  function predictNextAgent(currentNode) {
    if (currentNode === "synthesizer") return null;
    const idx = PIPELINE_ORDER.indexOf(currentNode);
    if (idx >= 0 && idx < PIPELINE_ORDER.length - 1) {
      return PIPELINE_ORDER[idx + 1];
    }
    // After critic, we don't know yet — could be synthesizer or loop back
    if (currentNode === "critic") return null;
    return null;
  }

  // Group steps by loop iteration
  const loops = [];
  let currentLoopSteps = [];
  let prevLoop = 0;
  for (const step of steps) {
    if (step.loop !== prevLoop) {
      if (currentLoopSteps.length > 0) {
        loops.push({ loop: prevLoop, steps: currentLoopSteps });
      }
      currentLoopSteps = [];
      prevLoop = step.loop;
    }
    currentLoopSteps.push(step);
  }
  if (currentLoopSteps.length > 0) {
    loops.push({ loop: prevLoop, steps: currentLoopSteps });
  }

  return (
    <div className="page timeline-page">
      <div className="timeline-header">
        <h2>Agent Pipeline</h2>
        {phase === "running" && (
          <span className="pipeline-status running">
            <span className="status-dot" />
            Processing
          </span>
        )}
        {phase === "done" && (
          <span className="pipeline-status complete">✓ Complete</span>
        )}
        {phase === "error" && (
          <span className="pipeline-status failed">✕ Failed</span>
        )}
      </div>
      <p className="timeline-question">"{question}"</p>

      <div className="timeline">
        {loops.map((loopGroup, loopIdx) => (
          <div key={loopIdx} className="timeline-loop-group">
            {loopGroup.loop > 0 && (
              <div className="loop-separator">
                <div className="loop-arrow">
                  <svg
                    width="24"
                    height="24"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <path d="M17 1l4 4-4 4" />
                    <path d="M3 11V9a4 4 0 0 1 4-4h14" />
                    <path d="M7 23l-4-4 4-4" />
                    <path d="M21 13v2a4 4 0 0 1-4 4H3" />
                  </svg>
                </div>
                <span className="loop-badge">Revision #{loopGroup.loop}</span>
                <span className="loop-label">Critic requested revisions</span>
              </div>
            )}

            {loopGroup.steps.map((step, i) => {
              const meta = AGENT_META[step.node] || {
                label: step.node,
                icon: "⚡",
                desc: "",
              };
              const isLast = i === loopGroup.steps.length - 1;

              return (
                <div
                  key={`${loopGroup.loop}-${step.node}-${i}`}
                  className={`timeline-step ${step.status} step-enter`}
                >
                  <div className="step-connector">
                    <div className="step-dot">
                      {step.status === "done" && "✓"}
                      {step.status === "active" && (
                        <span className="dot-pulse" />
                      )}
                      {step.status === "error" && "✕"}
                    </div>
                    {!isLast && <div className="step-line" />}
                  </div>

                  <div className="step-content">
                    <div className="step-header">
                      <span className="step-icon">{meta.icon}</span>
                      <span className="step-label">{meta.label}</span>
                      {step.durationMs != null && (
                        <span className="step-duration">
                          {step.durationMs < 1000
                            ? `${step.durationMs}ms`
                            : `${(step.durationMs / 1000).toFixed(1)}s`}
                        </span>
                      )}
                    </div>
                    <p className="step-desc">{meta.desc}</p>
                  </div>
                </div>
              );
            })}
          </div>
        ))}
      </div>

      {error && (
        <div className="error-banner">
          <p>Pipeline failed: {error}</p>
          <button onClick={() => navigate("/")}>← Try again</button>
        </div>
      )}
    </div>
  );
}
