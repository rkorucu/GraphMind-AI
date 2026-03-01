import { BrowserRouter, Routes, Route, Link } from "react-router-dom";
import Home from "./pages/Home";
import Timeline from "./pages/Timeline";
import Result from "./pages/Result";

export default function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <nav className="navbar">
          <Link to="/" className="nav-logo">
            <span className="logo-icon">🛡️</span>
            <span className="logo-text">Aegis</span>
          </Link>
        </nav>

        <main className="main-content">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/timeline" element={<Timeline />} />
            <Route path="/result" element={<Result />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
