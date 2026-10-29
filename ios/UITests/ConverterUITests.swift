import XCTest

final class ConverterUITests: XCTestCase {
    private var app: XCUIApplication!
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication(); app.launchArguments = ["-ui-testing", "-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
    }
    private func tap(_ id: String) {
        // SwiftUI List exposes selection rows as cells on some iOS versions.
        let button = app.descendants(matching: .any).matching(identifier: id).firstMatch
        if !button.waitForExistence(timeout: 8) { print(app.debugDescription); XCTFail("Missing \(id)"); return }
        if !button.isHittable {
            if ["swap", "fromAsset", "toAsset"].contains(id) { app.swipeDown() } else { app.swipeUp() }
        }
        button.tap()
    }
    private func choose(_ side: String, _ category: String, _ code: String) {
        app.swipeDown(); tap(side); tap(category + "Tab")
        if category == "World" {
            let search = app.searchFields.firstMatch; search.tap(); search.typeText(code)
        }
        tap("asset_" + code)
    }
    private func convert(_ expected: String) {
        tap("convert")
        let result = app.staticTexts["result"]
        let match = NSPredicate(format: "label == %@", expected)
        expectation(for: match, evaluatedWith: result); waitForExpectations(timeout: 10)
    }
    func testConversionFlow() {
        choose("fromAsset", "Metals", "XAU"); choose("toAsset", "World", "USD")
        convert("400,000.00 USD"); tap("swap"); convert("0.025 troy oz of Gold")
        XCTAssertEqual(app.textFields["amount"].value as? String, "100")
        choose("fromAsset", "Crypto", "BTC"); convert("2,500 troy oz of Gold")
        XCTAssertTrue(app.staticTexts["details"].label.contains("Different reference dates"))
        choose("toAsset", "Metals", "XAG"); convert("250,000 troy oz of Silver")
        choose("fromAsset", "Metals", "XPD"); choose("toAsset", "Metals", "XPD"); convert("100 troy oz of Palladium")
    }
    func testSearchAndWorldConversion() {
        tap("fromAsset")
        let search = app.searchFields.firstMatch; XCTAssertTrue(search.waitForExistence(timeout: 5)); search.tap(); search.typeText("BRL")
        tap("asset_BRL"); tap("toAsset")
        let targetSearch = app.searchFields.firstMatch; targetSearch.tap(); targetSearch.typeText("PHP"); tap("asset_PHP")
        convert("1,200.00 PHP")
    }
    func testLayoutAndFooter() {
        let from = app.buttons["fromAsset"].frame, to = app.buttons["toAsset"].frame, swap = app.buttons["swap"].frame
        XCTAssertGreaterThanOrEqual(swap.width, 44); XCTAssertGreaterThanOrEqual(swap.height, 44)
        XCTAssertEqual(swap.midX, from.midX, accuracy: 1)
        // Accessibility frames include the half-point outline around each card.
        XCTAssertGreaterThanOrEqual(swap.minY, from.maxY - 1); XCTAssertLessThanOrEqual(swap.maxY, to.minY + 1)
        let screenshot = XCTAttachment(screenshot: app.screenshot()); screenshot.name = "iOS Home"; screenshot.lifetime = .keepAlways; add(screenshot)
        for _ in 0..<3 where !app.buttons["about"].isHittable { app.swipeUp() }
        tap("about"); XCTAssertTrue(app.staticTexts["Version 4.0.2 · iOS"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "Clinton Hill")).firstMatch.exists)
    }
    func testLargeTextAndLandscapeFooter() {
        app.terminate()
        app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityL"]
        app.launch()
        XCUIDevice.shared.orientation = .landscapeLeft
        defer { XCUIDevice.shared.orientation = .portrait }
        for _ in 0..<8 where !app.buttons["about"].isHittable { app.swipeUp() }
        tap("about")
        XCTAssertTrue(app.staticTexts["Version 4.0.2 · iOS"].waitForExistence(timeout: 5))
    }
}
