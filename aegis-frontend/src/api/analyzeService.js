const API_BASE = "http://localhost:8080/api/agent";

export async function analyzeQuestion(question) {
  const response = await fetch(`${API_BASE}/analyze`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ question }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Analysis failed (${response.status}): ${errorText}`);
  }

  return response.json();
}

/**
 * Streams the agent pipeline via SSE.
 * Calls `onStep({ node, status, durationMs })` as each agent completes.
 * Returns a Promise that resolves with the final result
 * `{ analysis, confidence, traceId }` when the pipeline finishes.
 */
export function analyzeStream(question, onStep) {
  return new Promise((resolve, reject) => {
    fetch(`${API_BASE}/analyze-stream`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ question }),
    })
      .then((response) => {
        if (!response.ok) {
          return response.text().then((text) => {
            throw new Error(`Stream failed (${response.status}): ${text}`);
          });
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        function processChunk({ done, value }) {
          if (done) {
            // Stream closed without __DONE__ — shouldn't happen normally
            reject(new Error("Stream ended unexpectedly"));
            return;
          }

          buffer += decoder.decode(value, { stream: true });

          // Parse SSE lines from buffer
          const lines = buffer.split("\n");
          // Keep the last potentially incomplete line in buffer
          buffer = lines.pop() || "";

          let currentEvent = null;

          for (const line of lines) {
            if (line.startsWith("event:")) {
              currentEvent = line.slice(6).trim();
            } else if (line.startsWith("data:")) {
              const dataStr = line.slice(5).trim();
              if (!dataStr) continue;

              try {
                const data = JSON.parse(dataStr);

                if (currentEvent === "error") {
                  reject(new Error(data.error || "Pipeline failed"));
                  return;
                }

                if (currentEvent === "done" || data.node === "__DONE__") {
                  resolve({
                    analysis: data.analysis,
                    confidence: data.confidence,
                    traceId: data.traceId,
                  });
                  return;
                }

                if (currentEvent === "step") {
                  onStep(data);
                }
              } catch {
                // Malformed JSON line, skip
              }
              currentEvent = null;
            } else if (line.trim() === "") {
              currentEvent = null;
            }
          }

          reader.read().then(processChunk).catch(reject);
        }

        reader.read().then(processChunk).catch(reject);
      })
      .catch(reject);
  });
}
