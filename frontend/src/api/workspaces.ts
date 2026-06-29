import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'

export interface WorkspaceInfo {
  workspaceId: number
  ideaId: number
  title: string
  status: string
  creatorId: number
  creatorNickname: string
  creator: boolean
}

export interface WorkspaceMessage {
  id: number
  authorId: number
  authorNickname: string
  content: string
  createdAt: string
}

export const workspacesApi = {
  get: (workspaceId: number) =>
    unwrap(apiClient.get<ApiResponse<WorkspaceInfo>>(`/workspaces/${workspaceId}`)),

  getMessages: (workspaceId: number) =>
    unwrap(apiClient.get<ApiResponse<WorkspaceMessage[]>>(`/workspaces/${workspaceId}/messages`)),

  sendMessage: (workspaceId: number, content: string) =>
    unwrap(
      apiClient.post<ApiResponse<WorkspaceMessage>>(`/workspaces/${workspaceId}/messages`, {
        content,
      }),
    ),
}
