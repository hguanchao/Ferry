import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: ReactNode
  description?: ReactNode
  tag?: ReactNode
  extra?: ReactNode
}

export default function PageHeader({ title, description, tag, extra }: PageHeaderProps) {
  return (
    <div className="ferry-page-head">
      <div className="ferry-page-head__main">
        <div className="ferry-page-head__title">
          {title}
          {tag}
        </div>
        {description && <div className="ferry-page-head__desc">{description}</div>}
      </div>
      {extra && <div className="ferry-page-head__actions">{extra}</div>}
    </div>
  )
}
