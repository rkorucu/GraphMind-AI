import { useLocation, useNavigate } from "react-router-dom";

export default function Result() {
  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state;

  if (!state?.result) {
    return (
      <div className="page result-page">
        <p>No result available.</p>
        <button className="btn-primary" onClick={() => navigate("/")}>
          ← New analysis
        </button>
      </div>
    );
  }

  const { question, result } = state;
  const confidencePct = Math.round(result.confidence * 100);
  const confidenceClass =
    confidencePct >= 70 ? "high" : confidencePct >= 45 ? "medium" : "low";

  // Try to parse the analysis JSON for pretty display
  let parsedAnalysis = null;
  try {
    parsedAnalysis = JSON.parse(result.analysis);
  } catch {
    // raw string fallback
  }

  return (
    <div className="page result-page">
      <div className="result-header">
        <h2>Analysis Complete</h2>
        <div className="result-meta">
          <div className={`confidence-badge ${confidenceClass}`}>
            <span className="confidence-value">{confidencePct}%</span>
            <span className="confidence-label">confidence</span>
          </div>
          <div className="trace-badge">
            <span className="trace-label">Trace</span>
            <code>{result.traceId}</code>
          </div>
        </div>
      </div>

      <div className="result-question">
        <span className="label">Question</span>
        <p>{question}</p>
      </div>

      <div className="result-body">
        {parsedAnalysis ? (
          <div className="analysis-sections">
            {parsedAnalysis.synthesis && (
              <section className="analysis-card">
                <h3>Synthesis</h3>
                <p>{String(parsedAnalysis.synthesis)}</p>
              </section>
            )}

            {parsedAnalysis.recommendation && (
              <section className="analysis-card highlight">
                <h3>💡 Recommendation</h3>
                <p>{String(parsedAnalysis.recommendation)}</p>
              </section>
            )}

            {Array.isArray(parsedAnalysis.keyPoints) && (
              <section className="analysis-card">
                <h3>Key Points</h3>
                <ul>
                  {parsedAnalysis.keyPoints.map((point, i) => (
                    <li key={i}>{point}</li>
                  ))}
                </ul>
              </section>
            )}

            {parsedAnalysis.inputSummary && (
              <section className="analysis-card muted">
                <h3>Input Summary</h3>
                <p>{String(parsedAnalysis.inputSummary)}</p>
              </section>
            )}
          </div>
        ) : (
          <pre className="raw-output">{result.analysis}</pre>
        )}
      </div>

      <div className="result-actions">
        <button className="btn-primary" onClick={() => navigate("/")}>
          ← New analysis
        </button>
      </div>
    </div>
  );
}
