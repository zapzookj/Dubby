import { request } from '@/api/client';
import type { ChatMessage, ChatQuota, ChatSendResponse, CursorPage } from '@/api/types';

export function getChatQuota(): Promise<ChatQuota> {
  return request<ChatQuota>('/chat/quota');
}

export function getChatMessages(cursor?: number | null): Promise<CursorPage<ChatMessage>> {
  const q = cursor ? `?cursor=${cursor}` : '';
  return request<CursorPage<ChatMessage>>(`/chat/messages${q}`);
}

export function sendChatMessage(clientMessageId: string, content: string): Promise<ChatSendResponse> {
  return request<ChatSendResponse>('/chat/messages', {
    method: 'POST',
    body: JSON.stringify({ clientMessageId, content }),
  });
}

export function saveChatMessage(messageId: number) {
  return request<{ messageId: number; saved: boolean; derbyMessage: string }>(
    `/chat/messages/${messageId}/save`,
    { method: 'POST' },
  );
}
