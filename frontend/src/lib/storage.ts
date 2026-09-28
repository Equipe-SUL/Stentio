import { Platform } from "react-native";
import * as SecureStore from "expo-secure-store";

const TOKEN_KEY = "stentio_token";
const REMEMBER_SESSION_KEY = "stentio_remember_session";

function isWeb(): boolean {
  return Platform.OS === "web";
}

async function getItem(key: string): Promise<string | null> {
  if (isWeb()) return null;
  return SecureStore.getItemAsync(key);
}

async function setItem(key: string, value: string): Promise<void> {
  if (isWeb()) return;
  await SecureStore.setItemAsync(key, value);
}

async function removeItem(key: string): Promise<void> {
  if (isWeb()) return;
  await SecureStore.deleteItemAsync(key);
}

export const storage = {
  getItem,
  setItem,
  removeItem,
  TOKEN_KEY,
  REMEMBER_SESSION_KEY,
};
