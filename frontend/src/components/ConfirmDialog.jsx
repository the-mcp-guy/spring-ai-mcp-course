import { useState } from 'react'

/**
 * The dialog Class 12's elicitation puts in front of a person.
 *
 * While this is open, an MCP tool call on the server is still running and waiting for the
 * answer. Both buttons send one, because leaving it unanswered parks a thread until the
 * agent's timeout.
 */
export default function ConfirmDialog({ message, onAnswer }) {
  const [note, setNote] = useState('')

  return (
    <div className="overlay">
      <div className="dialog">
        <h3>Confirm</h3>
        <p>{message}</p>

        <label>
          Note (optional)
          <input value={note} onChange={(e) => setNote(e.target.value)} />
        </label>

        <div className="actions">
          <button className="danger" onClick={() => onAnswer(true, note)}>
            Yes, do it
          </button>
          <button onClick={() => onAnswer(false, note)}>No</button>
        </div>
      </div>
    </div>
  )
}
