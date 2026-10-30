import { useEffect, useMemo, useState } from 'react'

type Run = { id: string; job_name: string; params: string; status: string; started_at: string; finished_at: string | null; rows_written: number | null; error: string | null; seconds: number | null }
type Sale = { seller_id: string; sales_date: string; product: string; quantity: number }
type Summary = { seller_id: string; sales_date: string; item: string; total_quantity: number; sql_total: number | null; matches: boolean }
type Health = { status: string; database: boolean; sales?: number; summaries?: number; runs?: number }

const j = <T,>(u: string): Promise<T> => fetch(u).then(r => r.json())
const COLORS: Record<string, string> = { Beer: '#f59e0b', Food: '#10b981', Car: '#6366f1' }
const color = (p: string) => COLORS[p] ?? '#64748b'
const fmtTime = (s: string) => new Date(s + 'Z').toLocaleTimeString('en-CA', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false, timeZone: 'America/Edmonton' })

export default function App() {
  const [health, setHealth] = useState<Health | null>(null)
  const [runs, setRuns] = useState<Run[]>([])
  const [sales, setSales] = useState<Sale[]>([])
  const [summary, setSummary] = useState<Summary[]>([])
  const load = () => {
    j<Health>('/health').then(setHealth)
    j<Run[]>('/api/runs').then(setRuns)
    j<Sale[]>('/api/sales').then(setSales)
    j<Summary[]>('/api/summary').then(setSummary)
  }
  useEffect(() => { load(); const t = setInterval(load, 5000); return () => clearInterval(t) }, [])

  const dates = useMemo(() => [...new Set(summary.map(s => s.sales_date))].sort(), [summary])
  const sellers = useMemo(() => [...new Set(sales.map(s => s.seller_id))].sort(), [sales])
  const products = useMemo(() => [...new Set(sales.map(s => s.product))].sort(), [sales])
  const max = Math.max(1, ...summary.map(s => s.total_quantity))
  const ok = summary.filter(s => s.matches).length
  const done = runs.filter(r => r.status === 'SUCCEEDED').length

  return (
    <div className="page">
      <header>
        <div><h1>⚡ Spark Batch Runs</h1><div className="hint">Java 11 · Spark 3.0 (local) · Spring Boot 2.4 · PostgreSQL. Read-only viewer, refreshes every 5 s</div></div>
        <div className="badges">
          <span className={`badge ${health?.database ? 'ok' : 'bad'}`}>{health?.database ? 'PostgreSQL connected' : 'database unreachable'}</span>
          {health?.database && <span className="badge ok">{health.runs} runs · {health.sales} sales rows · {health.summaries} summaries</span>}
        </div>
      </header>

      <section className="card flow">
        <span>sales.json</span><i>→</i><span>ingesterJob · Spark flatMap</span><i>→</i><span>SALES</span><i>→</i>
        <span>salesSummaryJob · Spark JDBC read + reduce</span><i>→</i><span>SALES_SUMMARY</span>
        <i>·</i><span>JOB_RUN (every execution)</span>
      </section>

      <div className="cols2">
        <section className="card scroll">
          <h3>Run history <span className="hint">{done}/{runs.length} succeeded · times MT</span></h3>
          <table><thead><tr><th>Started</th><th>Job</th><th>Parameters</th><th>Status</th><th className="num">Rows</th><th className="num">Secs</th></tr></thead>
            <tbody>{runs.map(r => <tr key={r.id} title={r.error ?? ''}>
              <td>{fmtTime(r.started_at)}</td><td><b>{r.job_name}</b></td>
              <td><code>{r.params.split(' ').filter(p => !p.startsWith('jobName=')).join(' ')}</code></td>
              <td><span className={`st ${r.status}`}>{r.status}</span></td>
              <td className="num">{r.rows_written ?? ''}</td><td className="num">{r.seconds?.toFixed(1) ?? ''}</td></tr>)}</tbody></table>
        </section>
        <section className="card">
          <h3>Spark summary vs SQL check <span className="hint">{ok}/{summary.length} totals match a direct SQL SUM over SALES</span></h3>
          <div className="grid3">{dates.map(d => <div key={d}>
            <div className="hint" style={{ fontWeight: 700, marginBottom: 4 }}>{d}</div>
            {sellers.map(s => <div key={s} style={{ marginBottom: 6 }}>
              <div style={{ fontSize: 12, fontWeight: 600 }}>{s}</div>
              {summary.filter(x => x.sales_date === d && x.seller_id === s).map(x =>
                <div key={x.item} className="bar" style={{ gridTemplateColumns: '38px 1fr 44px', margin: '2px 0' }}>
                  <span className="hint">{x.item}</span>
                  <div style={{ width: `${x.total_quantity / max * 100}%`, background: color(x.item) }} />
                  <b>{x.total_quantity} <span className={x.matches ? 'ok' : 'no'}>{x.matches ? '✓' : '✗'}</span></b>
                </div>)}
            </div>)}
          </div>)}</div>
        </section>
      </div>

      <section className="card scroll">
        <h3>Ingested sales <span className="hint">SALES table written by ingesterJob from src/main/resources/data/ingestion/sales.json ({sales.length} line items)</span></h3>
        <table><thead><tr><th>Date</th>{sellers.map(s => <th key={s}>{s}</th>)}</tr></thead>
          <tbody>{[...new Set(sales.map(s => s.sales_date))].sort().map(d => <tr key={d}><td>{d}</td>
            {sellers.map(s => <td key={s}>{sales.filter(x => x.sales_date === d && x.seller_id === s).map((x, i) =>
              <span key={i} className="st" style={{ background: color(x.product) + '22', color: color(x.product), marginRight: 4 }}>{x.product} {x.quantity}</span>)}</td>)}</tr>)}</tbody></table>
        <div className="hint" style={{ marginTop: 6 }}>Products: {products.join(', ')}. Sample data shipped with the original course project.</div>
      </section>
    </div>
  )
}
