import type zhCN from './locales/zh-CN';

export type MessageSchema = typeof zhCN;
type MessagePaths<T> = {
  [K in keyof T & string]: T[K] extends string ? K : `${K}.${MessagePaths<T[K]>}`
}[keyof T & string];

export type MessageKey = MessagePaths<MessageSchema>;

declare module 'vue-i18n' {
  export interface DefineLocaleMessage extends MessageSchema {}
}
