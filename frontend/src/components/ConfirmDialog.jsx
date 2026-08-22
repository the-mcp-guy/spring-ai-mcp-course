import { useEffect, useState } from 'react'

/**
 * The dialog Class 12's elicitation puts in front of a person.
 *
 * While this is open, an MCP tool call on the server is still running and waiting for the
 * answer. Both buttons send one, because leaving it unanswered parks a thread until the
 * agent's timeout, which is what the countdown shows: the agent sends the number of
 * seconds it will wait, and at zero it stops waiting and the tool is told the question
 * was dismissed.
 */
export default function ConfirmDialog({ message, seconds, onAnswer }) {
  const [note, setNote] = useState('')
  const [secondsLeft, setSecondsLeft] = useState(seconds ?? null)

  useEffect(() => {
    if (secondsLeft === null || secondsLeft <= 0) return
    const timer = setTimeout(() => setSecondsLeft((n) => n - 1), 1000)
    return () => clearTimeout(timer)
  }, [secondsLeft])

  const expired = secondsLeft !== null && secondsLeft <= 0

  return (
    <div className="overlay">
      <div className="dialog">
        <h3>Confirm</h3>
        <p>{message}</p>

        {secondsLeft !== null && (
          <p className="countdown">
            {expired
              ? 'The question expired and the tool has stopped waiting.'
              : `${secondsLeft} seconds left to answer`}
          </p>
        )}

        <label>
          Note (optional)
          <input value={note} onChange={(e) => setNote(e.target.value)} />
        </label>

        <div className="actions">
          <button className="danger" disabled={expired} onClick={() => onAnswer(true, note)}>
            Yes, do it
          </button>
          <button disabled={expired} onClick={() => onAnswer(false, note)}>
            No
          </button>
        </div>
      </div>
    </div>
  )
}
