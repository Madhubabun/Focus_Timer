# Story video clips: what to make

The app can show AI-made video behind the timer in place of the drawn scene. You make one clip per story stage. The app doesn't play them like normal videos. It stretches each clip across its stage and shows the frame that matches the time left. Pausing freezes the picture, and resuming continues from the same moment.

If any stage clip is missing, the app uses the drawn scene for the whole story, so nothing breaks while clips are in progress.

## The seven clips

| File | Stage | Share of the session | At 25 min | What happens |
|---|---|---|---|---|
| `notice.mp4` | One is missing | 0–10% | 2.5 min | The flock grazes in a green valley. Jesus counts them, notices one is missing and looks out to the hills. |
| `search.mp4` | The search | 10–35% | 6.25 min | He takes up his staff, turns from the flock and walks out along a rocky path past olive trees. |
| `journey.mp4` | The journey | 35–60% | 6.25 min | He crosses a wide valley with a lake under a snowy peak and steps across a stream. The lost sheep is a speck far away. |
| `approach.mp4` | Drawing near | 60–80% | 5 min | He stops on a hill and sees the sheep. It lifts its head, takes a step, then walks toward him. |
| `found.mp4` | Found | 80–95% | 3.75 min | He kneels, gathers the sheep into his arms in warm golden light, then lifts it onto his shoulders. |
| `return.mp4` | Homeward | 95–100% | 1.25 min | He carries it back through the valley and the flock comes into view. |
| `complete.mp4` (optional) | The ending | after 0:00 | plays once | Wide shot: he reaches the flock, they gather round, and sunlight fills the valley. |

## Format

- MP4 (H.264), portrait 9:16, 1080 × 1920. The app crops the edges to fill the screen, so keep the action in the middle 80%.
- 24 or 30 frames per second. Sound is not needed; the app plays its own soft ambient audio.
- Make each clip as long as your tool allows. A 10-second clip stretched over 6 minutes changes picture only about every 2 seconds, so 30 to 60 seconds per stage looks much smoother. Most tools can extend a clip several times.
- Keep the camera slow and continuous, with no cuts inside a clip.
- Aim for 15 MB or less per clip.

## Keeping Jesus and the sheep the same in every clip

1. Make one reference picture of Jesus first, and reuse it as the character reference in every clip.
2. Make each clip by starting from the last frame of the previous one (image-to-video). The stages then flow into each other.
3. Give the lost sheep the same small detail every time, for example a small gold bell on a red cord.

## Prompts

Paste the style line in front of each stage prompt.

**Style line:** Painterly, warm, cinematic biblical landscape in soft natural sunlight. Lush green pastures, rolling hills, olive trees, wildflowers, a clear blue sky with white clouds, distant snow-capped mountains. Jesus has a kind, peaceful face, long dark wavy hair and a natural beard, and wears a white robe with a deep burgundy outer cloth, sandals, and carries a wooden shepherd's staff. Natural proportions, no halo or glowing effects. Realistic fluffy white sheep. Slow, gentle camera movement. Vertical 9:16.

- **notice:** Wide establishing shot of a flock of sheep grazing in a green valley. Jesus stands at the edge of the flock, slowly counting them, then pauses, notices one is missing and looks toward the distant hills with quiet concern.
- **search:** Jesus picks up his staff, turns away from the flock and begins walking along a winding rocky path between olive trees. The camera follows slowly from behind and to the side.
- **journey:** Jesus walks across a wide valley with a calm blue lake below a snow-capped peak, then steps across a small clear stream. Birds cross the sky. Far ahead, a tiny white sheep is barely visible on a distant hill.
- **approach:** Jesus stops on a grassy hilltop and sees a single white sheep with a small gold bell near a rock. The sheep lifts its head, notices him, takes a hesitant step, then walks toward him. The camera moves gently closer. His expression turns hopeful.
- **found:** Close, intimate shot in warm golden light. Jesus kneels and gently gathers the sheep with the gold bell into his arms, relieved and joyful, then rises and lifts it onto his shoulders.
- **return:** Jesus walks back through the valley carrying the sheep on his shoulders, peaceful and happy. The camera slowly pulls back and the waiting flock comes into view.
- **complete:** Wide valley shot as Jesus arrives with the sheep on his shoulders and the flock gathers around him. Warm afternoon sunlight fills the scene.

## Rights

Use a tool whose terms let you use the output in a published app, and keep a note of which tool and plan you used.

## Getting them into the app

Upload the files in the project thread. They go in `app/src/main/assets/stories/lost-sheep/` with the names above, and the app uses them automatically.
