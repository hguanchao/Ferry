import { Outlet } from 'react-router-dom'
import SideNav from './SideNav'
import TopBar from './TopBar'
import CommandPalette from './CommandPalette'

export default function AppLayout() {
  return (
    <div className="ferry-shell">
      <SideNav />
      <div className="ferry-main">
        <TopBar />
        <main className="ferry-content">
          <div className="ferry-content__inner">
            <Outlet />
          </div>
        </main>
      </div>
      <CommandPalette />
    </div>
  )
}
