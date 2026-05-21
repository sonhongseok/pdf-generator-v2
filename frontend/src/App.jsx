// frontend/src/App.jsx
import React from 'react';
import './App.css';
import CertificateForm from './components/CertificateForm';

export default function App() {
  return (
    <div className="window-frame">
      <div className="menu-bar">
        <span className="menu-item">File</span>
      </div>
      <CertificateForm />
    </div>
  );
}
