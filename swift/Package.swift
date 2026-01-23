// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "UUIDv7",
    platforms: [
        .macOS(.v12),
        .iOS(.v15),
        .tvOS(.v15),
        .watchOS(.v8)
    ],
    products: [
        .library(
            name: "UUIDv7",
            targets: ["UUIDv7"]
        )
    ],
    targets: [
        .target(
            name: "UUIDv7",
            path: "Sources/UUIDv7"
        ),
        .testTarget(
            name: "UUIDv7Tests",
            dependencies: ["UUIDv7"],
            path: "Tests/UUIDv7Tests"
        )
    ]
)
