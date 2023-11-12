import { instantiate } from './alesia.uninstantiated.mjs';

await wasmSetup;

instantiate({ skia: Module['asm'] });
