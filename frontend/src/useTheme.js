import { useEffect, useState } from 'react'

// No stored preference means "follow the OS", which is what :root's
// `color-scheme: light dark` already does. The data-theme attribute is only set
// once the user has actually picked a side.
export default function useTheme() {
  const [chosen, setChosen] = useState(() => localStorage.getItem('theme'))

  useEffect(() => {
    if (chosen) document.documentElement.dataset.theme = chosen
  }, [chosen])

  const theme =
    chosen ?? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')

  const toggle = () => {
    const next = theme === 'dark' ? 'light' : 'dark'
    localStorage.setItem('theme', next)
    setChosen(next)
  }

  return [theme, toggle]
}
