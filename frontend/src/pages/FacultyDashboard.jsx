export default function FacultyDashboard() {
  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8 rounded-2xl bg-slate-900 px-6 py-4 text-white">
          <p className="text-xs uppercase tracking-[0.25em] text-blue-300">EXAMIQ</p>
          <h1 className="mt-2 text-3xl font-bold">Faculty dashboard</h1>
        </header>

        <div className="grid gap-6 md:grid-cols-3">
          <div className="card">
            <p className="text-sm text-slate-500">Papers uploaded</p>
            <p className="mt-2 text-3xl font-bold">18</p>
          </div>
          <div className="card">
            <p className="text-sm text-slate-500">Downloads</p>
            <p className="mt-2 text-3xl font-bold">2.8k</p>
          </div>
          <div className="card">
            <p className="text-sm text-slate-500">Verification</p>
            <p className="mt-2 text-3xl font-bold text-green-600">Approved</p>
          </div>
        </div>

        <div className="mt-8 card">
          <h2 className="mb-4 text-xl font-semibold">AI question paper generator</h2>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <select className="rounded-lg border border-slate-300 px-3 py-2.5"><option>Database Management Systems</option></select>
            <select className="rounded-lg border border-slate-300 px-3 py-2.5"><option>Medium</option></select>
            <input className="rounded-lg border border-slate-300 px-3 py-2.5" placeholder="Number of questions" />
            <button className="btn-primary">Generate draft</button>
          </div>
        </div>
      </div>
    </div>
  );
}
