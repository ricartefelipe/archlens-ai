'use client';

import { Layers } from 'lucide-react';
import { APP_LOGO_URL } from '@/lib/branding';

interface AppLogoProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeClasses = {
  sm: 'w-8 h-8 rounded-lg',
  md: 'w-14 h-14 rounded-2xl',
  lg: 'w-10 h-10 rounded-xl',
};

const iconSizes = {
  sm: 'w-5 h-5',
  md: 'w-8 h-8',
  lg: 'w-6 h-6',
};

export function AppLogo({ size = 'sm', className = '' }: AppLogoProps) {
  const boxClass = `${sizeClasses[size]} ${className}`.trim();

  if (APP_LOGO_URL) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={APP_LOGO_URL}
        alt=""
        className={`${boxClass} object-contain`}
      />
    );
  }

  return (
    <div className={`${boxClass} bg-primary flex items-center justify-center`}>
      <Layers className={`${iconSizes[size]} text-primary-foreground`} />
    </div>
  );
}
