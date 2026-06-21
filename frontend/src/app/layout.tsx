import type { Metadata } from "next";
import type { CSSProperties } from "react";
import { Geist, Geist_Mono } from "next/font/google";
import { APP_NAME, PRIMARY_COLOR } from "@/lib/branding";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: APP_NAME,
  description: "Multi-tenant Architectural Intelligence Platform",
};

const brandingStyle = PRIMARY_COLOR
  ? ({
      '--primary': PRIMARY_COLOR,
      '--ring': PRIMARY_COLOR,
      '--accent': PRIMARY_COLOR,
    } as CSSProperties)
  : undefined;

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="pt-BR"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
      style={brandingStyle}
    >
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
