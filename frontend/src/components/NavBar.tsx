import { NavLink } from 'react-router-dom';

export function NavBar() {
  return (
    <nav className="app-nav">
      <strong className="brand">Tonique Vocal School</strong>
      <NavLink to="/" end className={({ isActive }) => (isActive ? 'active' : '')}>
        Журнал
      </NavLink>
      <NavLink to="/students" className={({ isActive }) => (isActive ? 'active' : '')}>
        Студенти
      </NavLink>
      <NavLink to="/tariffs" className={({ isActive }) => (isActive ? 'active' : '')}>
        Тарифи
      </NavLink>
      <NavLink to="/unmatched" className={({ isActive }) => (isActive ? 'active' : '')}>
        Неопрацьовані платежі
      </NavLink>
      <NavLink to="/admin" className={({ isActive }) => (isActive ? 'active' : '')}>
        Адміністрування
      </NavLink>
    </nav>
  );
}
