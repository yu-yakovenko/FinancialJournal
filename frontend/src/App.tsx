import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { NavBar } from './components/NavBar';
import { JournalPage } from './pages/JournalPage';
import { StudentsPage } from './pages/StudentsPage';
import { TariffsPage } from './pages/TariffsPage';
import { UnmatchedPaymentsPage } from './pages/UnmatchedPaymentsPage';
import { AdminPage } from './pages/AdminPage';

function App() {
  return (
    <BrowserRouter>
      <NavBar />
      <Routes>
        <Route path="/" element={<JournalPage />} />
        <Route path="/students" element={<StudentsPage />} />
        <Route path="/tariffs" element={<TariffsPage />} />
        <Route path="/unmatched" element={<UnmatchedPaymentsPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
