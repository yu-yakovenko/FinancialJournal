import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom';
import { NavBar } from './components/NavBar';
import { JournalPage } from './pages/JournalPage';
import { StudentsPage } from './pages/StudentsPage';
import { TariffsPage } from './pages/TariffsPage';
import { PaymentsPage } from './pages/PaymentsPage';
import { UnmatchedPaymentsPage } from './pages/UnmatchedPaymentsPage';
import { AdminPage } from './pages/AdminPage';
import { LoginPage } from './pages/LoginPage';

function AppLayout() {
  const location = useLocation();
  return (
    <>
      {location.pathname !== '/login' && <NavBar />}
      <Routes>
        <Route path="/" element={<JournalPage />} />
        <Route path="/students" element={<StudentsPage />} />
        <Route path="/tariffs" element={<TariffsPage />} />
        <Route path="/payments" element={<PaymentsPage />} />
        <Route path="/unmatched" element={<UnmatchedPaymentsPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppLayout />
    </BrowserRouter>
  );
}

export default App;
