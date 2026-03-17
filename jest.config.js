/** @type {import('jest').Config} */
module.exports = {
  testEnvironment: "node",
  transform: {
    "^.+\\.tsx?$": [
      "ts-jest",
      {
        tsconfig: {
          module: "commonjs",
          esModuleInterop: true,
          target: "es2020",
          strict: true,
          moduleResolution: "node",
        },
      },
    ],
  },
  testPathIgnorePatterns: ["/node_modules/", "/build/"],
};
