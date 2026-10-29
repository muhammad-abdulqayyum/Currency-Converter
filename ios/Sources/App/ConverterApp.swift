import SwiftUI

@main
struct ConverterApp: App {
    var body: some Scene {
        WindowGroup { ConverterView().preferredColorScheme(.light) }
    }
}

enum Theme {
    static let navy = Color(red: 16/255, green: 44/255, blue: 70/255)
    static let cream = Color(red: 250/255, green: 247/255, blue: 240/255)
    static let green = Color(red: 50/255, green: 103/255, blue: 78/255)
    static let muted = Color(red: 89/255, green: 104/255, blue: 115/255)
    static let line = Color(red: 222/255, green: 223/255, blue: 215/255)
    static let result = Color(red: 238/255, green: 242/255, blue: 233/255)
}

@MainActor final class ConverterModel: ObservableObject {
    @Published var assets: [Asset] = []
    @Published var from = Asset("USD", "US Dollar", "🇺🇸", .world)
    @Published var to = Asset("GBP", "British Pound", "🇬🇧", .world)
    @Published var amount = "100"
    @Published var result = "Ready to convert"
    @Published var details = "Choose your pair and tap Convert. Rate dates appear with results."
    @Published var error: String?
    @Published var loading = false
    private let repository: RateRepository
    init() {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-ui-testing") {
            repository = RateRepository(store: TestSnapshotStore(), download: TestRates.download)
        } else { repository = RateRepository() }
        #else
        repository = RateRepository()
        #endif
        do {
            guard let url = Bundle.main.url(forResource: "currencies", withExtension: "tsv") else { throw ConverterError.catalog }
            assets = try Catalog.load(String(contentsOf: url, encoding: .utf8))
        } catch { self.error = error.localizedDescription }
    }
    var sourceUnit: String {
        switch from.category {
        case .world: return "\(from.code) · \(from.name)"
        case .metals: return "Troy ounces of \(from.name) · 1 troy oz = 31.1034768 g"
        case .crypto: return "\(from.code) · units of \(from.name)"
        }
    }
    var note: String {
        var lines: [String] = []
        if from.category == .metals || to.category == .metals { lines.append("Metals use troy ounces. Dealer premiums are excluded.") }
        if from.category == .crypto || to.category == .crypto { lines.append("Crypto uses dated Swiss reference values, not live prices.") }
        return lines.joined(separator: "\n")
    }
    func clear() { result = "Ready to convert"; details = "Choose your pair and tap Convert. Rate dates appear with results."; error = nil }
    func swap() { guard !loading else { return }; let previous = from; from = to; to = previous; clear() }
    func select(_ asset: Asset, source: Bool) { if source { from = asset } else { to = asset }; clear() }
    func convert() {
        guard !loading else { return }
        let value: Decimal
        do { value = try Conversion.amount(amount) } catch { self.error = error.localizedDescription; return }
        let source = from, target = to
        error = nil
        if source.code == target.code {
            result = Conversion.display(value, asset: target, positiveInput: value > 0, exact: true)
            details = "Same asset · exact amount. No rate lookup needed."; return
        }
        loading = true; result = "Getting your rate…"; details = "Checking reference values…"
        Task {
            defer { loading = false }
            do {
                let first = try await repository.get(source), second = try await repository.get(target)
                let output = try Conversion.cross(value, source: first.rate, target: second.rate, digits: Conversion.digits(target))
                result = Conversion.display(output, asset: target, positiveInput: value > 0)
                details = first.details(code: source.code) + "\n\n" + second.details(code: target.code)
                if source.code != "USD", target.code != "USD", first.date != second.date {
                    details += "\n\nDifferent reference dates · indicative comparison."
                }
            } catch { result = "Rate unavailable"; details = error.localizedDescription }
        }
    }
}

struct ConverterView: View {
    @StateObject private var model = ConverterModel()
    @State private var picker: PickerSide?
    @State private var about = false
    @FocusState private var amountFocused: Bool
    @Environment(\.sizeCategory) private var textSize
    struct PickerSide: Identifiable { let source: Bool; var id: Bool { source } }
    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    header
                    Text("A world within reach.").font(.system(.title, design: .serif)).foregroundColor(Theme.navy).padding(.top, 14)
                    Text("World currencies, metals and crypto.").font(.subheadline).foregroundColor(Theme.muted).padding(.top, 4)
                    amountCard.padding(.top, 16)
                    currencyPair
                    if !model.note.isEmpty { Text(model.note).font(.footnote).foregroundColor(Theme.muted).padding(.top, 8) }
                    if let error = model.error { Text(error).font(.footnote).foregroundColor(.red).accessibilityIdentifier("inputError").padding(.top, 8) }
                    convertButton
                    resultCard
                    Spacer(minLength: 24)
                    footer(width: geometry.size.width - 40)
                }.frame(maxWidth: 580).padding(.horizontal, 20).padding(.vertical, 12)
                    .frame(maxWidth: .infinity, minHeight: geometry.size.height, alignment: .top)
            }.background(Theme.cream.ignoresSafeArea())
        }.sheet(item: $picker) { side in
            AssetPicker(assets: model.assets, chosen: side.source ? model.from : model.to, source: side.source) { model.select($0, source: side.source) }
        }.sheet(isPresented: $about) { AboutView() }
            .onChange(of: model.amount) { _ in model.clear() }
    }
    private var currencyPair: some View {
        VStack(alignment: .leading, spacing: 0) {
                    Text("From").font(.subheadline).foregroundColor(Theme.muted).padding(.top, 16).padding(.bottom, 5)
                    assetButton(model.from, source: true)
                    ZStack(alignment: .bottomLeading) {
                        Text("To").font(.subheadline).foregroundColor(Theme.muted)
                        HStack {
                            Spacer()
                            Button { amountFocused = false; model.swap() } label: {
                                Image(systemName: "arrow.left.arrow.right").font(.system(size: 14, weight: .medium))
                                    .frame(width: 30, height: 30).background(Theme.cream)
                                    .overlay(Circle().stroke(Theme.navy, lineWidth: 1)).clipShape(Circle())
                                    .frame(width: 48, height: 48).contentShape(Rectangle())
                            }.buttonStyle(.plain).foregroundColor(Theme.navy).accessibilityLabel("Swap source and destination")
                                .accessibilityHint("Keeps the entered amount").accessibilityIdentifier("swap").disabled(model.loading)
                            Spacer()
                        }
                    }.frame(minHeight: 48).padding(.bottom, 5)
                    assetButton(model.to, source: false)
        }
    }
    private var convertButton: some View {
                    Button { amountFocused = false; model.convert() } label: {
                        HStack { Spacer(); if model.loading { ProgressView().tint(.white) }; Text(model.loading ? "Checking…" : "Convert"); Spacer() }
                            .font(.headline).foregroundColor(.white).padding(.vertical, 15).frame(minHeight: 52).background(Theme.navy).cornerRadius(16)
                    }.buttonStyle(.plain).disabled(model.loading || model.assets.isEmpty).accessibilityIdentifier("convert").padding(.top, 14)
    }
    private var resultCard: some View {
                    VStack(alignment: .leading, spacing: 7) {
                        Text("YOUR CONVERSION").font(.caption).foregroundColor(Theme.green)
                        Text(model.result).font(.system(.title2, design: .serif)).foregroundColor(Theme.navy).textSelection(.enabled).accessibilityIdentifier("result")
                        Text(model.details).font(.footnote).foregroundColor(Theme.muted).textSelection(.enabled).accessibilityIdentifier("details")
                    }.frame(maxWidth: .infinity, alignment: .leading).padding(16).background(Theme.result).cornerRadius(16).padding(.top, 14)
    }
    private var header: some View {
        HStack(spacing: 12) {
            Image("Emblem").resizable().scaledToFit().frame(width: 44, height: 44).accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 3) {
                Text("UNIVERSAL\nCURRENCY CONVERTER").font(.system(.caption, design: .default).weight(.bold)).foregroundColor(Theme.navy)
                Text("VERSION 4 · FOR HUMANITY").font(.system(size: 10, weight: .bold)).tracking(1).foregroundColor(Color(red: 0.61, green: 0.15, blue: 0.22))
            }
        }.accessibilityElement(children: .combine)
    }
    private var amountCard: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text("Amount").font(.subheadline).foregroundColor(Theme.muted)
            TextField("Amount", text: $model.amount).font(.system(.largeTitle)).foregroundColor(Theme.navy)
                .keyboardType(.decimalPad).focused($amountFocused).disabled(model.loading)
                .accessibilityLabel("Amount").accessibilityIdentifier("amount").frame(minHeight: 44)
                .toolbar { ToolbarItemGroup(placement: .keyboard) { Spacer(); Button("Done") { amountFocused = false } } }
            Rectangle().fill(Theme.muted.opacity(0.6)).frame(height: 1)
            Text(model.sourceUnit).font(.footnote).foregroundColor(Theme.muted).accessibilityIdentifier("sourceUnit")
        }.padding(14).background(Color.white.opacity(0.7)).overlay(RoundedRectangle(cornerRadius: 16).stroke(Theme.line)).cornerRadius(16)
    }
    private func assetButton(_ asset: Asset, source: Bool) -> some View {
        Button { amountFocused = false; picker = PickerSide(source: source) } label: {
            HStack(spacing: 12) {
                AssetIcon(asset: asset).frame(width: 40, height: 40).accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 3) {
                    Text(asset.name).font(.system(.headline, design: .serif)).foregroundColor(Theme.navy)
                    Text(asset.subtitle).font(.footnote).foregroundColor(Theme.muted)
                }.frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.down").font(.system(size: 14, weight: .semibold)).foregroundColor(Theme.green)
            }.padding(.horizontal, 14).padding(.vertical, 10).frame(minHeight: 64).frame(maxWidth: .infinity)
                .background(Color.white.opacity(0.7)).overlay(RoundedRectangle(cornerRadius: 16).stroke(Theme.line)).cornerRadius(16)
        }.buttonStyle(.plain).disabled(model.loading).accessibilityLabel("\(source ? "From" : "To"): \(asset.name), \(asset.subtitle)")
            .accessibilityHint("Choose any world currency, metal or crypto").accessibilityIdentifier(source ? "fromAsset" : "toAsset")
    }
    private func footer(width: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Rectangle().fill(Theme.line).frame(height: 1)
            HStack(spacing: 10) {
                Image("HeritageFlag").resizable().scaledToFit().frame(width: 52, height: 35).accessibilityLabel("Black American Heritage Flag")
                Rectangle().fill(Theme.line).frame(width: 1, height: 32)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Built for humanity, with care.").font(.caption.weight(.bold)).foregroundColor(Theme.navy)
                    Text("Muhammad Hafiz Abdul-Qayyum").font(.caption2).foregroundColor(Theme.muted)
                }.frame(maxWidth: .infinity, alignment: .leading)
                if width >= 380 && !textSize.isAccessibilityCategory { aboutButton }
            }
            if width < 380 || textSize.isAccessibilityCategory { HStack { Spacer(); aboutButton } }
        }
    }
    private var aboutButton: some View {
        Button("About & sources") { amountFocused = false; about = true }.font(.caption).foregroundColor(Theme.green)
            .frame(minWidth: 96, minHeight: 44).accessibilityIdentifier("about")
    }
}
