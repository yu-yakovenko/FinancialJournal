import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { NavBar } from './components/NavBar';
import { JournalPage } from './pages/JournalPage';
import { StudentsPage } from './pages/StudentsPage';
import { UnmatchedPaymentsPage } from './pages/UnmatchedPaymentsPage';

function App() {
  return (
    <BrowserRouter>
      <NavBar />
      <Routes>
        <Route path="/" element={<JournalPage />} />
        <Route path="/students" element={<StudentsPage />} />
        <Route path="/unmatched" element={<UnmatchedPaymentsPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
