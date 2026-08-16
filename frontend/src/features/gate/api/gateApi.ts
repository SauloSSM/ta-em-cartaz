import { listPublicEvents, type PublicEventResponse } from '../../events';

export type GateEvent = PublicEventResponse;

export async function listGateEvents(search?: string): Promise<GateEvent[]> {
  const response = await listPublicEvents(search);
  return response.events;
}
