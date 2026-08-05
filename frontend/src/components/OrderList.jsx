const money = (amount) =>
  new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'GBP' }).format(amount ?? 0)

export default function OrderList({ orders, selected, onSelect }) {
  return (
    <section className="orders">
      <h2>Orders ({orders.length})</h2>

      <ul>
        {orders.map((order) => (
          <li
            key={order.orderId}
            className={order.orderId === selected?.orderId ? 'order selected' : 'order'}
            onClick={() => onSelect(order)}
          >
            <span className="id">{order.orderId}</span>
            <span className={`badge ${order.status.toLowerCase()}`}>{order.status}</span>
            <span className="who">{order.customer?.name}</span>
            <span className="total">{money(order.totalAmount)}</span>
          </li>
        ))}
      </ul>

      {selected && (
        <div className="detail">
          <h3>{selected.orderId}</h3>
          <table>
            <tbody>
              {selected.items.map((item) => (
                <tr key={item.productId}>
                  <td>{item.productName}</td>
                  <td>&times;{item.quantity}</td>
                  <td>{money(item.unitPrice)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {selected.shipment?.carrier ? (
            <p className="shipment">
              {selected.shipment.carrier} {selected.shipment.trackingNumber}, due{' '}
              {selected.shipment.estimatedDelivery}
            </p>
          ) : (
            <p className="shipment muted">Not shipped yet</p>
          )}
        </div>
      )}
    </section>
  )
}
