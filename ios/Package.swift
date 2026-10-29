// swift-tools-version: 5.7
import PackageDescription
let package = Package(name: "ConverterCore", platforms: [.iOS(.v15), .macOS(.v12)],
    products: [.library(name: "ConverterCore", targets: ["ConverterCore"])],
    targets: [.target(name: "ConverterCore", path: "Sources/Core"),
              .testTarget(name: "ConverterCoreTests", dependencies: ["ConverterCore"], path: "Tests")])
