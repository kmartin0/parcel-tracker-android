package com.km.parceltracker.ui.parcels.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.km.parceltracker.R
import com.km.parceltracker.enums.ParcelSearchingEnum
import com.km.parceltracker.ui.parcels.ParcelsViewModel
import kotlinx.android.synthetic.main.bottom_sheet_parcel_search_by.*

class SearchByBottomDialogFragment : BottomSheetDialogFragment() {

    private lateinit var viewModel: ParcelsViewModel
    private var initialButtonResId: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_parcel_search_by, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(activity as AppCompatActivity).get(ParcelsViewModel::class.java)
        setInitialCheckedButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        applySelection()
    }

    private fun applySelection() {
        if (initialButtonResId != rgSorting.checkedRadioButtonId) {
            when (rgSorting.checkedRadioButtonId) {
                R.id.rbTitle -> viewModel.searchBy.value = ParcelSearchingEnum.TITLE
                R.id.rbSender -> viewModel.searchBy.value = ParcelSearchingEnum.SENDER
                R.id.rbCourier -> viewModel.searchBy.value = ParcelSearchingEnum.COURIER
            }
        }
    }

    private fun setInitialCheckedButton() {
        rgSorting.check(
            when (viewModel.searchBy.value) {
                null, ParcelSearchingEnum.TITLE -> R.id.rbTitle
                ParcelSearchingEnum.SENDER -> R.id.rbSender
                ParcelSearchingEnum.COURIER -> R.id.rbCourier
            }
        )
        initialButtonResId = rgSorting.checkedRadioButtonId
    }

}