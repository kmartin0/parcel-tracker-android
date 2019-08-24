package com.km.parceltracker.ui.parcels

import android.app.Application
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.km.parceltracker.base.BaseViewModel
import com.km.parceltracker.repository.ParcelRepository
import com.km.parceltracker.enums.ParcelSearchingEnum
import com.km.parceltracker.enums.ParcelSortingEnum
import com.km.parceltracker.enums.SortOrderEnum
import com.km.parceltracker.model.Parcel
import com.km.parceltracker.model.ParcelsSortAndFilterConfig
import com.km.parceltracker.repository.SettingsRepository
import com.km.parceltracker.util.SingleLiveEvent
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.doAsync

class ParcelsViewModel(application: Application) : BaseViewModel(application) {

    private val parcelRepository = ParcelRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)

    private var repoParcels = MutableLiveData<List<Parcel>>()
    var parcels = MediatorLiveData<List<Parcel>>()
    var sortAndFilterConfig = MutableLiveData<ParcelsSortAndFilterConfig>().apply {
        value = settingsRepository.getSortAndFilterSettings()
    }
    var error = SingleLiveEvent<String>()

    private fun setupParcelSources() {
        // Retrieve the parcels from the repository and add the value in repoParcels
        parcelRepository.getParcels()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<List<Parcel>> {
                override fun onSuccess(t: List<Parcel>) {
                    stopLoading()
                    repoParcels.value = t
                }

                override fun onSubscribe(d: Disposable) {
                    disposables.add(d)
                    startLoading()
                }

                override fun onError(e: Throwable) {
                    stopLoading()
                    handleApiError(e)
                }
            })

        // When the value of repoParcels is changed then sort and filter the list and set the value of parcels to it
        parcels.addSource(repoParcels) {
            parcels.value = sortAndFilterParcels(it, sortAndFilterConfig.value)
        }

        // When the value of sortAndFilterConfig is changed then sort and filter repoParcels and set the value of parcels to it
        parcels.addSource(sortAndFilterConfig) { config ->
            repoParcels.value?.let { parcels ->
                this@ParcelsViewModel.parcels.value = sortAndFilterParcels(parcels, config)
            }
        }
    }

    /**
     * Delete the [parcel] from the [parcelRepository]
     */
    fun deleteParcel(parcel: Parcel) {
        doAsync {
            parcelRepository.deleteParcel(parcel)
        }
    }

    /**
     * @return List<Parcel> sorted and filtered parcels list using [sortAndFilterConfig] for the sorting and filter options
     */
    private fun sortAndFilterParcels(
        parcels: List<Parcel>?,
        sortAndFilterConfig: ParcelsSortAndFilterConfig?
    ): List<Parcel>? {
        return sortParcels(filterParcels(parcels, sortAndFilterConfig), sortAndFilterConfig)
    }

    /**
     * @return List<Parcel> Sorted parcels list using [sortAndFilterConfig] for sorting options.
     */
    private fun sortParcels(
        parcels: List<Parcel>?,
        sortAndFilterConfig: ParcelsSortAndFilterConfig?
    ): List<Parcel>? {
        // Return the parcels list if no parcels or sorting configuration is provided.
        // Otherwise use the sortBy attribute of the sortAndFilterConfig to determine by which attribute the list
        // should be sorted. Then use the sortOrder attribute to determine the sort order.
        return if (parcels == null || sortAndFilterConfig == null) parcels
        else when (sortAndFilterConfig.sortBy) {
            ParcelSortingEnum.TITLE -> {
                when (sortAndFilterConfig.sortOrder) {
                    SortOrderEnum.ASCENDING -> parcels.sortedBy { it.title.toLowerCase() }
                    SortOrderEnum.DESCENDING -> parcels.sortedByDescending { it.title.toLowerCase() }
                }
            }
            ParcelSortingEnum.SENDER -> {
                when (sortAndFilterConfig.sortOrder) {
                    SortOrderEnum.ASCENDING -> parcels.sortedBy { it.sender?.toLowerCase() }
                    SortOrderEnum.DESCENDING -> parcels.sortedByDescending { it.sender?.toLowerCase() }
                }
            }
            ParcelSortingEnum.COURIER -> {
                when (sortAndFilterConfig.sortOrder) {
                    SortOrderEnum.ASCENDING -> parcels.sortedBy { it.courier?.toLowerCase() }
                    SortOrderEnum.DESCENDING -> parcels.sortedByDescending { it.courier?.toLowerCase() }
                }
            }
            ParcelSortingEnum.DATE -> {
                when (sortAndFilterConfig.sortOrder) {
                    SortOrderEnum.ASCENDING -> parcels.sortedBy { it.lastUpdated }
                    SortOrderEnum.DESCENDING -> parcels.sortedByDescending { it.lastUpdated }
                }
            }
            ParcelSortingEnum.STATUS -> {
                when (sortAndFilterConfig.sortOrder) {
                    SortOrderEnum.ASCENDING -> parcels.sortedBy { it.parcelStatus.status }
                    SortOrderEnum.DESCENDING -> parcels.sortedByDescending { it.parcelStatus.status }
                }
            }
        }
    }

    /**
     * @return List<Parcel> Filtered parcels list using [sortAndFilterConfig] for filter options.
     */
    private fun filterParcels(
        parcels: List<Parcel>?,
        sortAndFilterConfig: ParcelsSortAndFilterConfig?
    ): List<Parcel>? {
        // Return the parcels list if no parcels or sorting configuration is provided.
        // If no search query is provided only filter by parcel status.
        // Otherwise filter by search query and parcel status.
        return if (parcels == null || sortAndFilterConfig == null) parcels
        else if (sortAndFilterConfig.searchQuery.isNullOrBlank()) filterParcelStatus(parcels, sortAndFilterConfig)
        else {
            parcels.filter { parcel ->
                when (sortAndFilterConfig.searchBy) { // Find the attribute to filter by. Then use the searchQuery and parcel status to filter.
                    ParcelSearchingEnum.TITLE -> {
                        sortAndFilterConfig.isParcelStatusSelected(parcel) &&
                                parcel.title.toLowerCase().contains(sortAndFilterConfig.searchQuery!!.toLowerCase())
                    }
                    ParcelSearchingEnum.SENDER -> {
                        if (parcel.sender.isNullOrBlank()) false
                        else sortAndFilterConfig.isParcelStatusSelected(parcel) &&
                                parcel.sender!!.toLowerCase().contains(sortAndFilterConfig.searchQuery!!.toLowerCase())
                    }
                    ParcelSearchingEnum.COURIER -> {
                        if (parcel.sender.isNullOrBlank()) false
                        else sortAndFilterConfig.isParcelStatusSelected(parcel) &&
                                parcel.sender!!.toLowerCase().contains(sortAndFilterConfig.searchQuery!!.toLowerCase())
                    }
                }
            }
        }
    }

    /**
     * @return List<Parcel> Filtered parcels list by Parcel Status.
     */
    private fun filterParcelStatus(
        parcels: List<Parcel>?,
        sortAndFilterConfig: ParcelsSortAndFilterConfig?
    ): List<Parcel>? {
        return if (parcels == null || sortAndFilterConfig == null) parcels
        else {
            parcels.filter { parcel ->
                sortAndFilterConfig.isParcelStatusSelected(parcel)
            }
        }
    }

    fun setSortingAndFilterConfig(sortAndFilterConfig: ParcelsSortAndFilterConfig) {
        settingsRepository.setSortAndFilterSettings(sortAndFilterConfig)
        this.sortAndFilterConfig.value = sortAndFilterConfig
    }

    fun refresh() {
        if (isLoading.value == false) {
            parcels.removeSource(repoParcels)
            parcels.removeSource(sortAndFilterConfig)
            setupParcelSources()
        }
    }
}