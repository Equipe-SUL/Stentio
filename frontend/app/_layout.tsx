import { Slot } from 'expo-router';

// Mude de ../global.css para ./global.css (com apenas um ponto)
import './global.css'; 

export default function RootLayout() {
  return <Slot />;
}