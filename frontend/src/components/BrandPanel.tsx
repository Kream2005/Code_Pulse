import { useI18n } from '../i18n/I18nContext';

export default function BrandPanel() {
  const { t } = useI18n();
  return (
    <div className="relative hidden w-1/2 flex-col justify-between overflow-hidden bg-gradient-to-br from-brand-dark via-brand to-brand-light p-14 text-white lg:flex">
      <div className="blob pointer-events-none absolute -right-32 -top-32 h-96 w-96 bg-brand-light/40 blur-3xl" />
      <div className="blob pointer-events-none absolute top-1/3 -left-24 h-80 w-80 bg-white/10 blur-3xl" />
      <div className="blob pointer-events-none absolute -bottom-24 right-1/4 h-72 w-72 bg-white/10 blur-3xl" />
      <div className="blob pointer-events-none absolute -bottom-40 -left-40 h-[32rem] w-[32rem] border border-white/15" />
      <div className="blob pointer-events-none absolute -bottom-28 -left-28 h-96 w-96 border border-white/10" />

      <img
        src="/flower-logo.png"
        alt=""
        className="pointer-events-none absolute -bottom-16 -right-16 w-96 opacity-10 brightness-0 invert"
      />

      <div
        className="pointer-events-none absolute inset-0 opacity-[0.06]"
        style={{
          backgroundImage:
            'linear-gradient(#fff 1px, transparent 1px), linear-gradient(90deg, #fff 1px, transparent 1px)',
          backgroundSize: '44px 44px',
        }}
      />

      <div className="flex items-center">
        <img src="/logo.png" alt={t('appName')} className="relative w-70 brightness-0 invert" />
      </div>

      <div className="relative">
        <h1 className="text-5xl font-bold leading-tight tracking-tight">{t('appName')}</h1>
        <h1 className="mt-6 text-5xl leading-[1.05] tracking-tight">
          {t('brand.tagline1')}
          <br />
          <span className="text-white/70">{t('brand.tagline2')}</span>
        </h1>
        <p className="mt-6 max-w-md text-lg leading-relaxed text-white/75">{t('brand.description')}</p>
      </div>

      <p className="relative text-sm text-white/60">© 2026 {t('appName')}</p>
    </div>
  );
}
