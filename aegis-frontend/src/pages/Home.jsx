import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { analyzeQuestion } from "../api/analyzeService";

export default function Home() {
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!question.trim()) return;

    setLoading(true);
    setError(null);

    // Navigate to timeline immediately to show progress
    navigate("/timeline", { state: { question } });
  };

  const exampleQuestions = [
    "Should a startup enter the AI insurance market?",
    "Is renewable energy a good long-term investment?",
    "Should we adopt a microservices architecture?",
  ];

  return (
    <div className="page home-page">
      <div className="hero">
        <div className="hero-badge">Multi-Agent Decision Engine</div>
        <h1>
          <span className="gradient-text">Aegis</span> Analysis
        </h1>
        <p className="hero-subtitle">
          Ask a strategic question. Five AI agents will research, evaluate
          risks, critique, and synthesise an actionable answer.
        </p>
      </div>

      <form className="question-form" onSubmit={handleSubmit}>
        <div className="input-wrapper">
          <textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="Enter your strategic question…"
            rows={3}
            disabled={loading}
          />
          <button type="submit" disabled={loading || !question.trim()}>
            {loading ? <span className="spinner" /> : <>Analyze →</>}
          </button>
        </div>
        {error && <div className="error-message">{error}</div>}
      </form>

      <div className="examples">
        <span className="examples-label">Try:</span>
        {exampleQuestions.map((q) => (
          <button
            key={q}
            className="example-chip"
            onClick={() => setQuestion(q)}
          >
            {q}
          </button>
        ))}
      </div>
    </div>
  );
}
