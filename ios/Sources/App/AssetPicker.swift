import SwiftUI

/// One shared bundled atlas preserves the same artwork on both platforms.
private enum PremiumArtwork {
    static let assets = UIImage(named: "PremiumAssets")?.cgImage
    static let extras = UIImage(named: "PremiumExtras")?.cgImage
    static func image(_ code: String) -> UIImage? {
        let codes = ["XAU", "XAG", "XPT", "XPD", "BTC", "ETH", "XRP", "LTC"]
        let extra = code == "METALS" || code == "BCH"
        guard let atlas = extra ? extras : assets else { return nil }
        let columns = extra ? 2 : 4, rows = extra ? 1 : 2
        let index = extra ? (code == "METALS" ? 0 : 1) : (codes.firstIndex(of: code) ?? 4)
        let x = index % columns, y = index / columns
        let rect = CGRect(x: x * atlas.width / columns, y: y * atlas.height / rows,
                          width: (x+1)*atlas.width/columns - x*atlas.width/columns,
                          height: (y+1)*atlas.height/rows - y*atlas.height/rows)
        guard let cell = atlas.cropping(to: rect) else { return nil }
        return UIImage(cgImage: cell)
    }
}
private struct PremiumIcon: View {
    let code: String
    var body: some View {
        if let image = PremiumArtwork.image(code) { Image(uiImage: image).resizable().scaledToFit() }
    }
}


struct AssetIcon: View {
    let asset: Asset
    var body: some View {
        Group {
            if asset.category == .world { Text(asset.symbol).font(.system(size: 30)) }
            else if asset.category == .metals {
                PremiumIcon(code: asset.code).frame(width: 40, height: 40)
            } else {
                ZStack(alignment: .bottomTrailing) {
                    if ["BTC", "BCH", "ETH", "LTC", "XRP"].contains(asset.rateCode) {
                        PremiumIcon(code: asset.rateCode).frame(width: 40, height: 40)
                    } else {
                        ZStack {
                            Circle().fill(LinearGradient(colors: [Color.yellow.opacity(0.5), .orange], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .overlay(Circle().stroke(Color.orange)).frame(width: 36, height: 36)
                            Text(asset.code == "SAT" ? "₿" : asset.symbol).font(.system(size: 22, weight: .bold)).foregroundColor(Theme.navy)
                        }
                    }
                    if asset.isSubunit {
                        Text(asset.code == "SAT" ? "sat" : "drop").font(.system(size: 8, weight: .bold)).foregroundColor(.white)
                            .padding(.horizontal, 3).padding(.vertical, 2).background(Theme.navy).cornerRadius(3)
                    }
                }
            }
        }
    }
}
struct AssetPicker: View {
    let assets: [Asset], chosen: Asset
    let source: Bool
    let select: (Asset) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var category: Category
    @State private var search = ""
    init(assets: [Asset], chosen: Asset, source: Bool, select: @escaping (Asset) -> Void) {
        self.assets = assets; self.chosen = chosen; self.source = source; self.select = select
        _category = State(initialValue: chosen.category)
    }
    private var matches: [Asset] {
        assets.filter { $0.category == category && (search.isEmpty || ($0.name + " " + $0.code).localizedCaseInsensitiveContains(search)) }
    }
    var body: some View {
        NavigationView {
            VStack(spacing: 12) {
                HStack(spacing: 8) {
                    ForEach(Category.allCases) { item in
                        Button { category = item; search = "" } label: {
                            VStack(spacing: 5) {
                                Group {
                                    if item == .world { Text("🌎").font(.system(size: 30)) }
                                    else { PremiumIcon(code: item == .metals ? "METALS" : "BTC") }
                                }.frame(width: 36, height: 36).accessibilityHidden(true)
                                Text(item.rawValue).font(.subheadline.weight(.medium))
                            }.frame(maxWidth: .infinity).padding(.vertical, 9)
                                .background(category == item ? Theme.navy : Theme.cream).cornerRadius(12)
                                .foregroundColor(category == item ? .white : Theme.navy)
                        }.buttonStyle(.plain).accessibilityIdentifier(item.rawValue + "Tab")
                            .accessibilityAddTraits(category == item ? [.isSelected] : [])
                    }
                }.padding(.horizontal, 16)
                if matches.isEmpty { Text("No matches in this category.").foregroundColor(Theme.muted).padding(); Spacer() }
                else {
                    List(matches) { asset in
                        Button { select(asset); dismiss() } label: {
                            HStack(spacing: 12) {
                                AssetIcon(asset: asset).frame(width: 40, height: 40).accessibilityHidden(true)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(asset.name).font(.system(.headline, design: .serif)).foregroundColor(Theme.navy)
                                    Text(asset.subtitle).font(.footnote).foregroundColor(Theme.muted)
                                }
                                Spacer()
                                if asset == chosen { Image(systemName: "checkmark").foregroundColor(Theme.green) }
                            }.padding(.vertical, 5)
                        }.accessibilityIdentifier("asset_" + asset.code).accessibilityLabel(asset.name + ", " + asset.subtitle)
                            .accessibilityAddTraits(asset == chosen ? [.isSelected] : [])
                    }.listStyle(.plain)
                }
            }.padding(.top, 8).background(Theme.cream)
                .navigationTitle(source ? "Convert from" : "Convert to").navigationBarTitleDisplayMode(.inline)
                .searchable(text: $search, placement: .navigationBarDrawer(displayMode: .always), prompt: "Search name or code")
                .toolbar { ToolbarItem(placement: .navigationBarTrailing) { Button("Done") { dismiss() }.foregroundColor(Theme.green) } }
        }.navigationViewStyle(.stack)
    }
}

struct AboutView: View {
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text("Universal Currency Converter").font(.system(.title2, design: .serif)).foregroundColor(Theme.navy)
                    Text("Version 4.0.4 · iOS")
                    Text("Created by Muhammad Hafiz Abdul-Qayyum\nFrom Clinton Hill, Brooklyn, New York City, USA")
                    Text("“From your Brother Muhammad Hafiz Abdul-Qayyum in the Wilderness of North America.”")
                    Text("For humanity. If Allah wills.")
                    Text("The original sun, crescent and star emblem celebrates light, possibility and connection.")
                    Text("Black American Heritage Flag: designed by Melvin Charles and Gleason T. Jackson (1967). SVG rendition by Xophe84, Wikimedia Commons (CC0).")
                    Text("Data: Frankfurter and its official-source providers. Crypto: Swiss Federal Tax Administration (ESTV). These are dated reference conversions, not executable trading quotes. Both source dates are shown. Different dates are labeled as indicative comparisons.")
                    Text("Metals use troy ounces (31.1034768 g). Crypto uses coin units. Satoshi: 100,000,000 per BTC. Drops: 1,000,000 per XRP. Subunit outputs round to whole units. Dealer premiums, transaction fees and spreads are excluded. Flags identify currencies and are not a statement about sovereignty.")
                    Text("Only reference-rate snapshots are cached on this device, reused for six hours. A failed refresh may use a saved quote up to seven days old, clearly labeled. Amounts and conversion history are not saved. No ads or analytics SDKs are included.")
                    Link("Frankfurter sources and documentation", destination: URL(string: "https://frankfurter.dev")!)
                }.font(.body).foregroundColor(Theme.muted).padding(20).frame(maxWidth: 600)
            }.background(Theme.cream).navigationTitle("About & sources").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .navigationBarTrailing) { Button("Done") { dismiss() } } }
        }.navigationViewStyle(.stack)
    }
}
