import { FaBell, FaBolt } from 'react-icons/fa6'

function BrandMark({ compact = false }) {
  return (
    <div
      className={`relative flex items-center justify-center rounded-[22px] bg-[radial-gradient(circle_at_top,_#67e8f9_0%,_#22d3ee_35%,_#0f172a_100%)] text-white shadow-[0_16px_35px_rgba(14,165,233,0.28)] ${
        compact ? 'h-10 w-10' : 'h-11 w-11'
      }`}
    >
      <FaBell className={compact ? 'text-sm' : 'text-base'} />
      <span className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-white text-[10px] text-cyan-600 shadow-sm">
        <FaBolt />
      </span>
    </div>
  )
}

export default BrandMark
