import { create } from 'zustand'

const COLLAPSE_KEY = 'ferry.sider.collapsed'
const THEME_KEY = 'ferry.theme'

type ThemeMode = 'light' | 'dark'

function readCollapsed() {
  try {
    return localStorage.getItem(COLLAPSE_KEY) === '1'
  } catch {
    return false
  }
}

function readTheme(): ThemeMode {
  try {
    const stored = localStorage.getItem(THEME_KEY)
    if (stored === 'light' || stored === 'dark') return stored
  } catch {
    /* ignore */
  }
  return 'light'
}

interface UiState {
  collapsed: boolean
  theme: ThemeMode
  paletteOpen: boolean
  toggleCollapsed: () => void
  setCollapsed: (value: boolean) => void
  setTheme: (value: ThemeMode) => void
  toggleTheme: () => void
  setPaletteOpen: (value: boolean) => void
}

export const useUiStore = create<UiState>((set, get) => ({
  collapsed: readCollapsed(),
  theme: readTheme(),
  paletteOpen: false,

  toggleCollapsed() {
    const next = !get().collapsed
    try {
      localStorage.setItem(COLLAPSE_KEY, next ? '1' : '0')
    } catch {
      /* ignore */
    }
    set({ collapsed: next })
  },

  setCollapsed(value) {
    try {
      localStorage.setItem(COLLAPSE_KEY, value ? '1' : '0')
    } catch {
      /* ignore */
    }
    set({ collapsed: value })
  },

  setTheme(value) {
    try {
      localStorage.setItem(THEME_KEY, value)
    } catch {
      /* ignore */
    }
    document.documentElement.dataset.theme = value
    set({ theme: value })
  },

  toggleTheme() {
    get().setTheme(get().theme === 'dark' ? 'light' : 'dark')
  },

  setPaletteOpen(value) {
    set({ paletteOpen: value })
  },
}))
