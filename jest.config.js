/** @type {import('jest').Config} */
module.exports = {
  testEnvironment: "node",
  transform: {
    "^.+\\.tsx?$": [
      "ts-jest",
      {
        tsconfig: {
          module: "node18",
          moduleResolution: "node16",
          esModuleInterop: true,
          target: "es2020",
          strict: true,
        },
      },
    ],
  },
  testPathIgnorePatterns: ["/node_modules/", "/build/"],
};
