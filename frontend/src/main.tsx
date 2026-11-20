import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { AuthPage } from './pages/AuthPage'
import { ListingDetailPage } from './pages/ListingDetailPage'
import { BookingPage, CheckoutPage } from './pages/BookingPage'
import { HowItWorksPage, HostPage } from './pages/InfoPage'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route element={<Navigate replace to="/explore" />} path="/" />
        <Route element={<App />} path="/explore" />
        <Route element={<HowItWorksPage />} path="/how-it-works" />
        <Route element={<HostPage />} path="/host" />
        <Route element={<ListingDetailPage />} path="/listings/:id" />
        <Route element={<BookingPage />} path="/listings/:id/book" />
        <Route element={<CheckoutPage />} path="/listings/:id/checkout" />
        <Route element={<AuthPage mode="login" />} path="/login" />
        <Route element={<AuthPage mode="register" />} path="/register" />
        <Route element={<Navigate replace to="/explore" />} path="*" />
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)
