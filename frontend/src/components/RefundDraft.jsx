import { useState } from 'react'

const REASONS = ['arrived damaged', 'arrived late', 'faulty', 'no longer needed', 'wrong item']
const REFUNDABLE = ['SHIPPED', 'DELIVERED', 'CANCELLED']

/**
 * The "Draft refund email" button on the selected order, and the dialog behind it.
 *
 * The endpoint it posts to is built in Class 8, so before that the button answers
 * with an error message, the same arrangement as the chat panel before Class 7.
 * Orders that have not shipped cannot be refunded through this path; for those
 * the button is disabled and the tooltip says what a support person does instead.
 */
export default function RefundDraft({ order }) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [draft, setDraft] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [copied, setCopied] = useState(false)

  const eligible = REFUNDABLE.includes(order.status)

  function close() {
    setOpen(false)
    setReason('')
    setDraft(null)
    setError(null)
    setBusy(false)
    setCopied(false)
  }

  async function requestDraft(event) {
    event.preventDefault()
    if (!reason.trim() || busy) return
    setBusy(true)
    setError(null)

    try {
      const response = await fetch('/api/refund-email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ orderId: order.orderId, reason: reason.trim() }),
      })
      if (response.status === 404) throw new Error('The refund endpoint is built in Class 8.')
      if (!response.ok) throw new Error(`support-agent returned ${response.status}`)
      const body = await response.json()
      setDraft(body.reply)
    } catch (e) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  async function copy() {
    await navigator.clipboard.writeText(draft)
    setCopied(true)
  }

  return (
    <>
      <button className="refund" disabled={!eligible} onClick={() => setOpen(true)}>
        Draft refund email
      </button>
      {!eligible && (
        <p className="refund-hint">
          Cancel the order instead; cancelling is what starts a refund.
        </p>
      )}

      {open && (
        <div className="overlay">
          <div className="dialog">
            {draft === null ? (
              <form onSubmit={requestDraft}>
                <h3>Draft a refund email for {order.orderId}</h3>
                <label>
                  Reason for the refund
                  <input
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Type a reason, or pick one below"
                    autoFocus
                  />
                </label>
                <div className="reasons">
                  {REASONS.map((r) => (
                    <button
                      key={r}
                      type="button"
                      className={r === reason ? 'tab active' : 'tab'}
                      onClick={() => setReason(r)}
                    >
                      {r}
                    </button>
                  ))}
                </div>
                {error && <p className="error">{error}</p>}
                <div className="actions">
                  <button type="button" onClick={close}>
                    Cancel
                  </button>
                  <button type="submit" className="primary" disabled={busy || !reason.trim()}>
                    {busy ? 'Drafting…' : 'Draft the email'}
                  </button>
                </div>
              </form>
            ) : (
              <>
                <h3>Refund email for {order.orderId}</h3>
                <pre className="draft">{draft}</pre>
                <div className="actions">
                  <button type="button" onClick={close}>
                    Close
                  </button>
                  <button type="button" className="primary" onClick={copy}>
                    {copied ? 'Copied' : 'Copy'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </>
  )
}
