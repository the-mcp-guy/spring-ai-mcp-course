import { useEffect, useRef, useState } from 'react'
import ConfirmDialog from './ConfirmDialog.jsx'

/**
 * The chat panel.
 *
 * Nothing is behind it until Class 7 builds support-agent, so an unreachable backend is
 * reported rather than treated as an error. Progress notifications (Class 11) and
 * confirmation requests (Class 12) arrive on an SSE stream the agent opens.
 */
export default function ChatPanel({ order }) {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const [progress, setProgress] = useState(null)
  const [confirmation, setConfirmation] = useState(null)
  const [agentUp, setAgentUp] = useState(null)
  const conversationId = useRef(`ui-${Math.random().toString(36).slice(2)}`)

  // Class 11 and 12 push through here. Absent before Class 7, which is not an error.
  useEffect(() => {
    let source
    try {
      source = new EventSource(`/api/events?conversationId=${conversationId.current}`)
      source.onopen = () => setAgentUp(true)
      source.onerror = () => setAgentUp(false)
      source.addEventListener('progress', (e) => setProgress(JSON.parse(e.data)))
      source.addEventListener('log', (e) =>
        setMessages((m) => [...m, { role: 'log', text: JSON.parse(e.data).message }]),
      )
      source.addEventListener('confirmation', (e) => setConfirmation(JSON.parse(e.data)))
    } catch {
      setAgentUp(false)
    }
    return () => source?.close()
  }, [])

  async function send(event) {
    event.preventDefault()
    const message = input.trim()
    if (!message || busy) return

    setMessages((m) => [...m, { role: 'user', text: message }])
    setInput('')
    setBusy(true)
    setProgress(null)

    try {
      const response = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          conversationId: conversationId.current,
          message,
          orderId: order?.orderId ?? null,
        }),
      })
      if (!response.ok) throw new Error(`support-agent returned ${response.status}`)
      const body = await response.json()
      setMessages((m) => [...m, { role: 'agent', text: body.reply }])
      setAgentUp(true)
    } catch (e) {
      setAgentUp(false)
      setMessages((m) => [
        ...m,
        { role: 'error', text: `${e.message}. support-agent is built in Class 7.` },
      ])
    } finally {
      setBusy(false)
      setProgress(null)
    }
  }

  async function answerConfirmation(accepted, note) {
    await fetch(`/api/confirmations/${confirmation.id}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ confirmed: accepted, note: note ?? '' }),
    })
    setConfirmation(null)
  }

  return (
    <section className="chat">
      <h2>
        Ask the desk
        {agentUp === false && <span className="muted">: agent not running</span>}
      </h2>

      <div className="messages">
        {messages.length === 0 && (
          <p className="muted">
            Ask about {order ? order.orderId : 'an order'}. The agent that answers is built
            in Class 7.
          </p>
        )}
        {messages.map((m, i) => (
          <p key={i} className={`msg ${m.role}`}>
            {m.text}
          </p>
        ))}
      </div>

      {progress && (
        <div className="progress">
          <div className="bar" style={{ width: `${progress.percent}%` }} />
          <span>{progress.percent}%</span>
        </div>
      )}

      <form onSubmit={send}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={order ? `Where is ${order.orderId}?` : 'Ask a question'}
          disabled={busy}
        />
        <button type="submit" disabled={busy || !input.trim()}>
          {busy ? '…' : 'Send'}
        </button>
      </form>

      {confirmation && (
        <ConfirmDialog
          message={confirmation.message}
          seconds={confirmation.seconds}
          onAnswer={answerConfirmation}
        />
      )}
    </section>
  )
}
