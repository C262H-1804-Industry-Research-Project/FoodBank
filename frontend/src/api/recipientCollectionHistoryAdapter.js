// Convert backend collection-history data into the format used by the frontend.
export function toRecipientCollectionHistoryRow(item) {
    return {
        id: item.historyId,
        foodName: item.foodName || 'Unknown food item',
        category: item.category || 'Uncategorised',

        // The backend currently stores only a number and does not store a unit.
        quantity:
            item.quantityCollected == null
                ? ''
                : String(item.quantityCollected),

        donor: item.donorName || 'Unknown donor',
        collectedDate: formatCollectedDate(item.collectedDatetime),
    }
}

function formatCollectedDate(value) {
    if (!value) {
        return ''
    }

    // Example:
    // 2026-10-16T15:30:00 -> 2026-10-16 15:30
    return value.replace('T', ' ').slice(0, 16)
}