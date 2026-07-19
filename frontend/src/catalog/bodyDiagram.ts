export type BodyView = "front" | "back";

export interface BodyDiagramConfig {
  src: string;
  viewBox: string;
}

// Update `src` to match the files you drop into frontend/public/, and `viewBox`
// to the exact pixel dimensions of each image (e.g. from Inkscape's Document
// Properties). MuscleSubGroup.svgPathFront/svgPathBack must be digitized against
// this same coordinate space for the overlay to line up.
export const BODY_DIAGRAMS: Record<BodyView, BodyDiagramConfig> = {
  front: {
    src: "/body-front.png",
    viewBox: "0 0 480 901",
  },
  back: {
    src: "/body-back.png",
    viewBox: "0 0 153.6 288.32",
  },
};
