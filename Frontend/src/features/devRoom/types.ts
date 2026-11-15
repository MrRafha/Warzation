export type RoomState = {
  roomId: string;
  counter: number;
  version: number;
  updatedAt: string;
};

export type CommandResult = {
  commandId: string;
  roomId: string;
  type: string;
  amount: number;
  status: "APPLIED" | "DUPLICATE";
  processedAt: string;
  resultingCounter: number;
  resultingVersion: number;
  room: RoomState;
};

export type ErrorPayload = {
  code: string;
  message: string;
  fields?: Record<string, string>;
};

export type RoomCommand = {
  commandId: string;
  type: "INCREMENT_COUNTER";
  payload: { amount: number };
};
