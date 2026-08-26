import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './AccountSettings.css'

export default function AccountSettings() {
  const { user, logout } = useAuth()
  const [confirmLogout, setConfirmLogout] = useState(false)

  return (
    <div className="account-page">
      <header className="account-header">
        <Link to="/" className="back-link">← Back</Link>
        <h1>Account</h1>
      </header>

      <div className="account-section">
        <div className="account-row">
          <span className="account-label">Display name</span>
          <span className="account-value">{user?.displayName}</span>
        </div>
        <div className="account-row">
          <span className="account-label">Email</span>
          <span className="account-value">{user?.email}</span>
        </div>
      </div>

      <div className="account-section account-section--danger">
        {confirmLogout ? (
          <div className="account-confirm">
            <p>Are you sure you want to sign out?</p>
            <div className="account-confirm__actions">
              <button className="btn btn--danger" onClick={logout}>Yes, sign out</button>
              <button className="btn btn--ghost" onClick={() => setConfirmLogout(false)}>Cancel</button>
            </div>
          </div>
        ) : (
          <button className="btn btn--ghost btn--danger-outline" onClick={() => setConfirmLogout(true)}>
            Sign out
          </button>
        )}
      </div>
    </div>
  )
}
