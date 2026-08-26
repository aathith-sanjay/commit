import type { TreeStage, TreeState } from '../types'
import './TreeBadge.css'

const STAGE_EMOJI: Record<TreeStage, string> = {
  SEED: '🌱',
  HERB: '🌿',
  SHRUB: '🪴',
  SAPLING: '🌳',
  YOUNG_TREE: '🌲',
  TREE: '🌳',
  FLOWERING_TREE: '🌸',
  FRUIT_TREE: '🍎',
  MATURE_TREE: '🏔️',
}

const STAGE_LABEL: Record<TreeStage, string> = {
  SEED: 'Seed',
  HERB: 'Herb',
  SHRUB: 'Shrub',
  SAPLING: 'Sapling',
  YOUNG_TREE: 'Young Tree',
  TREE: 'Tree',
  FLOWERING_TREE: 'Flowering Tree',
  FRUIT_TREE: 'Fruit Tree',
  MATURE_TREE: 'Mature Tree',
}

interface Props {
  stage: TreeStage
  state: TreeState
  size?: 'sm' | 'lg'
}

export default function TreeBadge({ stage, state, size = 'sm' }: Props) {
  const dead = state === 'DEAD'
  return (
    <span className={`tree-badge tree-badge--${size} ${dead ? 'tree-badge--dead' : ''}`}>
      <span className="tree-badge__emoji">{STAGE_EMOJI[stage]}</span>
      <span className="tree-badge__label">{STAGE_LABEL[stage]}{dead ? ' 💀' : ''}</span>
    </span>
  )
}
