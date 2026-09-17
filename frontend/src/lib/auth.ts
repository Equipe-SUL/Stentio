import { storage } from "./storage";

export async function getToken(): Promise<string | null> {
  return storage.getItem(storage.TOKEN_KEY);
}

export async function setToken(token: string): Promise<void> {
  await storage.setItem(storage.TOKEN_KEY, token);
}

export async function clearToken(): Promise<void> {
  await storage.removeItem(storage.TOKEN_KEY);
}

export async function getRememberSession(): Promise<boolean> {
  const value = await storage.getItem(storage.REMEMBER_SESSION_KEY);
  return value === "true";
}

export async function setRememberSession(value: boolean): Promise<void> {
  if (value) {
    await storage.setItem(storage.REMEMBER_SESSION_KEY, "true");
  } else {
    await storage.removeItem(storage.REMEMBER_SESSION_KEY);
  }
}