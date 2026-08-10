import { useEffect, useState } from 'react'
import OrderList from './components/OrderList.jsx'
import ChatPanel from './components/ChatPanel.jsx'
import useTheme from './useTheme.js'

const STATUSES = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED']

export default function App() {
  const [status, setStatus] = useState('SHIPPED')
  const [orders, setOrders] = useState([])
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState(null)
  const [theme, toggleTheme] = useTheme()

  useEffect(() => {
    let cancelled = false
    setError(null)

    fetch(`/api/orders?status=${status}`)
      .then((response) => {
        if (!response.ok) throw new Error(`order-service returned ${response.status}`)
        return response.json()
      })
      .then((data) => {
        if (!cancelled) {
          setOrders(data)
          setSelected(null)
        }
      })
      .catch((e) => !cancelled && setError(e.message))

    return () => {
      cancelled = true
    }
  }, [status])

  return (
    <div className="app">
      <header>
        <div className="titlebar">
          <h1>Support Desk</h1>
          <button
            className="theme"
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {theme === 'dark' ? '☀' : '☾'}
          </button>
        </div>
        <nav>
          {STATUSES.map((s) => (
            <button
              key={s}
              className={s === status ? 'tab active' : 'tab'}
              onClick={() => setStatus(s)}
            >
              {s}
            </button>
          ))}
        </nav>
      </header>

      {error && (
        <p className="error">
          Could not reach order-service: {error}. Start it with{' '}
          <code>mvn -pl order-service spring-boot:run</code>.
        </p>
      )}

      <main>
        <OrderList orders={orders} selected={selected} onSelect={setSelected} />
        <ChatPanel order={selected} onClearOrder={() => setSelected(null)} />
      </main>
    </div>
  )
}
