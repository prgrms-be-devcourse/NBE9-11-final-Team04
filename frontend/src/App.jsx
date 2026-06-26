import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ApiConsole from './pages/ApiConsole';
import Dashboard from './pages/Dashboard';
import Ideas from './pages/Ideas';
import Login from './pages/Login';
import { ExpertPage, ProposerPage } from './pages/RolePages';
import { useAuth } from './hooks/useAuth';

function Private({ children }) { return useAuth().token ? children : <Navigate to="/login" replace />; }

export default function App() {
  return <Routes>
    <Route element={<Layout />}>
      <Route index element={<Navigate to="/ideas" replace />} />
      <Route path="login" element={<Login />} />
      <Route path="ideas" element={<Ideas />} />
      <Route path="api-console" element={<ApiConsole />} />
      <Route path="dashboard" element={<Private><Dashboard /></Private>} />
      <Route path="expert" element={<Private><ExpertPage /></Private>} />
      <Route path="proposer" element={<Private><ProposerPage /></Private>} />
    </Route>
  </Routes>;
}