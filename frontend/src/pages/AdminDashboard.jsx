export default function AdminDashboard() {
  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8 rounded-2xl bg-slate-900 px-6 py-4 text-white">
          <p className="text-xs uppercase tracking-[0.25em] text-blue-300">EXAMIQ</p>
          <h1 className="mt-2 text-3xl font-bold">Admin dashboard</h1>
        </header>

        <div className="grid gap-6 md:grid-cols-4">
          <div className="card"><p className="text-sm text-slate-500">Total users</p><p className="mt-2 text-3xl font-bold">1,248</p></div>
          <div className="card"><p className="text-sm text-slate-500">Student</p><p className="mt-2 text-3xl font-bold">1,104</p></div>
          <div className="card"><p className="text-sm text-slate-500">Faculty</p><p className="mt-2 text-3xl font-bold">122</p></div>
          <div className="card"><p className="text-sm text-slate-500">Approved papers</p><p className="mt-2 text-3xl font-bold">845</p></div>
        </div>

        <div className="mt-8 card">
          <h2 className="mb-4 text-xl font-semibold">Pending approvals</h2>
          <div className="overflow-hidden rounded-lg border border-slate-200">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-slate-50 text-slate-700">
                <tr>
                  <th className="px-4 py-3">Paper</th>
                  <th className="px-4 py-3">Uploader</th>
                  <th className="px-4 py-3">AI confidence</th>
                  <th className="px-4 py-3">Action</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-t border-slate-200">
                  <td className="px-4 py-3">Operating Systems</td>
                  <td className="px-4 py-3">ravi_92</td>
                  <td className="px-4 py-3">87%</td>
                  <td className="px-4 py-3"><button className="btn-primary">Review</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
