export const labelClass = (collapsed: boolean) =>
    `overflow-hidden whitespace-nowrap transition-all duration-300 ease-in-out ${
        collapsed ? "max-w-0 opacity-0" : "max-w-[160px] opacity-100"
    }`;