import './SkeletonCard.css'

export default function SkeletonCard() {
  return (
    <div className="skeleton-card">
      <div className="skeleton skeleton--title" />
      <div className="skeleton skeleton--text" />
      <div className="skeleton skeleton--btn" />
    </div>
  )
}
